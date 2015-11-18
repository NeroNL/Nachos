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
    int ID1 = exec("write.coff", 0, 0);
    int ID2 = exec("write.coff", 0, 0);
    println("ID1 is %d, and ID2 is %d.", ID1, ID2);
    return 0;
}