int
main(int argc, char *argv[])
{
    /*int paramIn;
    
    if (argc != 1) {
        return -1;
    }
    
    paramIn = *(argv[0]);
    
    exit( paramIn );*/
    
    int r = 2;
    
    printf ("writing with an invalid buffer (should not crash, only return an error)...\n");
    r = write (1, (char *) 0x81, 10);
    if (r < 0) {
        printf ("...passed (r = %d)\n", r);
    } else {
        printf ("...failed (r = %d)\n", r);
    }

}