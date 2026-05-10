# QRCode
Class to Generate a QR code in the command line from a given URL. Medium strength (15%) error correction. Works up to a version 13 QR Code, which allows for a length 331 input.


To use, download all files and compile the Generator.

```Bash
# Directly
javac Generator.java
# Using Makefile
make generator
# Using the Makefile's default target
make
```
This will create all necessary compiled files.

To use in the command line:

```bash
java Generator [<url>] [csv|png] [<filename>]
```  

If run with no arguments, you will be prompted with
```Bash
Enter your URL: 
```

Once you enter your URL, a QR Code will be printed in the command line that you can take a screenshot of.

Alternatively, you can enter your url as a command line argument.
```Bash
java Generator "www.github.com"
```
You can pass an additional argument to have the code output to a csv or png rather than printing
```Bash
java Generator "www.github.com" csv
```
You can also pass a name of the csv/png to be created, otherwise you will be prompted
```Bash
java Generator "www.github.com" png githubCode.png
```
If a .csv/.png extension, respectively, is not given, one will be added

The csv will have '1's representing the dark squares and '0's representing light squares. This can be copied into sheets/numbers/excel and colored with conditional formatting rules. The PNG will look the same as the printed output.

All arguments require every previous one or the program won't work properly.