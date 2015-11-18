/*
 * write.c
 *
 * Write a string to stdout, one byte at a time.  Does not require any
 * of the other system calls to be implemented.
 */

#include "syscall.h"

int
main (int argc, char *argv[])
{

	creat("test");
	open("test");

    char *str = "\nroses are red\nviolets are blue\nI love Nachos\nand so do you\n\n";
    char *s1 = "";
    
    while (*str) {
	write (1, str, 1);
	write (2, str, 1);
	write (3, str, 1);
	str++;
    }

    //read(1, str, 1);
    read(2, s1, 1);
    read(3, s1, 1);

    return 0;
}