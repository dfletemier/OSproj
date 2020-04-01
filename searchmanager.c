#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/msg.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include<string.h>
#include "longest_word_search.h"
#include "queue_ids.h"
#include<unistd.h>
#include<signal.h>
#include<semaphore.h>
#include<ctype.h>

#ifndef mac
size_t                  /* O - Length of string */
strlcpy(char       *dst,        /* O - Destination string */
        const char *src,      /* I - Source string */
        size_t      size)     /* I - Size of destination string buffer */
{
    size_t    srclen;         /* Length of source string */


    /*
     * Figure out how much room is needed...
     */

    size --;

    srclen = strlen(src);

    /*
     * Copy the appropriate amount...
     */

    if (srclen > size)
        srclen = size;

    memcpy(dst, src, srclen);
    dst[srclen] = '\0';

    return (srclen);
}
#endif

sem_t countMutex; 
int totalCount = 0;
int passageCount = 0;
int prefixCount;
char **prefixes; 

/*This handler is used when we haven't gotten a single response*/
void other_handler(){
    for(int i=0; i<prefixCount; i++){
        printf("%s - pending\n", prefixes[i]);
    }
}
/*This handler uses countMutex so that totalCount won't be updated at the same time. ait determines the amount of responses remaining based on the number re
ceived and the number of passages/prefixes there are */
void sig_handler(int signo)
{
  if (signo == SIGINT){
      
     if(passageCount == 0){
          other_handler();
      }
      else{
        sem_wait(&countMutex);
      for(int i = 0; i<prefixCount; i++)
      {
         if(totalCount >= passageCount*(i+1)){
              printf("%s - done\n", prefixes[i]);
          }
          else if(totalCount <= (passageCount*i)){
              printf("%s - pending\n",prefixes[i]);
          }
          else printf("%s - %d of %d\n", prefixes[i],(totalCount-(i*passageCount)), passageCount);
      }
    }
        sem_post(&countMutex);
  }
  }


int main(int argc, char**argv)
{
    if (signal(SIGINT, sig_handler) == SIG_ERR)
    printf("\ncan't catch SIGINT\n");
    int msqid;
    int msgflg = IPC_CREAT | 0666;
    key_t key;
    prefix_buf sbuf;
    response_buf rbuf;
    size_t buf_length;
    int ret;

    //initializing mutex
    sem_init(&countMutex, 1, 1); 

    if (argc <= 2 || strlen(argv[2]) <2 ) { 
       fprintf(stderr,"Error: please provide sleep time and prefix of at least two characters for search\n");
        fprintf(stderr, "Usage: %s <prefix>\n",argv[0]);
        exit(-1);
    }
    prefixCount = argc-2; 
    prefixes = (char **)malloc(prefixCount * sizeof(char *)); 
    for (int i=0; i<prefixCount; i++) {
        for(int j = 0; j < strlen(argv[i+2]); j++){
            argv[i+2][j] = tolower(argv[i+2][j]); //must make sure that the prefix is lowercase. This is the best way in C.
        }
         prefixes[i] = (char *)malloc(strlen(argv[i+2])* sizeof(char)); 
        strcpy(prefixes[i], argv[i+2]);
    }

   key = ftok(CRIMSON_ID,QUEUE_NUMBER);
    if ((msqid = msgget(key, msgflg)) < 0) {
        int errnum = errno;
        fprintf(stderr, "Value of errno: %d\n", errno);
        perror("(msgget)");
        fprintf(stderr, "Error msgget: %s\n", strerror( errnum ));
    }
    
    //Send prefix and receive responses for each prefix
    for(int i = 2; i < argc; i++){
         if (strlen(argv[i]) <= 2 || strlen(argv[i]) > 20) { 
        fprintf(stderr, "Error: prefix length invalid.. skipping %s\n",argv[i]); 
        }
    else{
     // We'll send message type 1
    sbuf.mtype = 1;
    strlcpy(sbuf.prefix,argv[i],WORD_LENGTH);
    sbuf.id= i-1;
    buf_length = strlen(sbuf.prefix) + sizeof(int)+1;//struct size without long int type

    // Send a message.
    if((msgsnd(msqid, &sbuf, buf_length, IPC_NOWAIT)) < 0) {
        int errnum = errno;
        fprintf(stderr,"%d, %ld, %s, %d\n", msqid, sbuf.mtype, sbuf.prefix, (int)buf_length);
        perror("(msgsnd)");
        fprintf(stderr, "Error sending msg: %s\n", strerror( errnum ));
        exit(1);
    }
    else
        printf("\nMessage(%d): \"%s\" Sent (%d bytes)\n\n", sbuf.id, sbuf.prefix,(int)buf_length);
    
    int counter = 0;
    
    response_buf *results;
    //get results and store them in an array of response bufs for each of the passages.
    do{
    do {
      ret = msgrcv(msqid, &rbuf, sizeof(response_buf), 2, 0);//receive type 2 message
      int errnum = errno;
      if (ret < 0 && errno !=EINTR){
        fprintf(stderr, "Value of errno: %d\n", errno);
        perror("Error printed by perror");
        fprintf(stderr, "Error receiving msg: %s\n", strerror( errnum ));
      }
    } while ((ret < 0 ) && (errno == 4));
    //fprintf(stderr,"msgrcv error return code --%d:$d--",ret,errno);
    passageCount = rbuf.count;
    if(counter == 0){
        //allocate space in the array based on the number of passages...
        results = (response_buf *)malloc(passageCount * sizeof(response_buf));
    }
        results[rbuf.index] = rbuf;
        counter++;
        sem_wait(&countMutex);
        totalCount++;
        sem_post(&countMutex);
       
    } while(counter < passageCount);
    //Print report now that we have received all of the responses
    printf("Report \"%s\"\n", sbuf.prefix);
    for(int j = 0; j<passageCount; j++){
        response_buf theBuf = results[j];
        if(theBuf.present ==1)
        printf("Passage %d - %s - %s\n", theBuf.index, theBuf.location_description, theBuf.longest_word);
        else
        printf("Passage %d - %s - no word found\n", theBuf.index, theBuf.location_description);
    }
    
    free(results);
    //sleep if we aren't on the last prefix.
    if(i != argc-1){
        sleep(strtol(argv[1], NULL, 10));
    }
    }
    }

    for(int i = 0; i < prefixCount; ++i){
        free(prefixes[i]);
    }
    free(prefixes);

    // Send a message telling passage processor to quit.
    sbuf.mtype = 1;
    strlcpy(sbuf.prefix,"   ",WORD_LENGTH);
    sbuf.id = 0;
    buf_length = strlen(sbuf.prefix) + sizeof(int)+1;//struct size without long int type
    if((msgsnd(msqid, &sbuf, buf_length, IPC_NOWAIT)) < 0) {
        int errnum = errno;
        fprintf(stderr,"%d, %ld, %s, %d\n", msqid, sbuf.mtype, sbuf.prefix, (int)buf_length);
        perror("(msgsnd)");
        fprintf(stderr, "Error sending msg: %s\n", strerror( errnum ));
        exit(1);
    }
     else
        printf("\nMessage(%d): \"%s\" Sent (%d bytes)\n\n", sbuf.id, sbuf.prefix,(int)buf_length);

    printf("Exiting...\n\n");
    sem_destroy(&countMutex);
    exit(0);

}