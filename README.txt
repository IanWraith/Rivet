Rivet is a free open source decoder of various HF data modes which interest members 
of the Enigma 2000 group.

http://groups.yahoo.com/group/enigma2000/

Currently the program decodes the modes XPA , XPA2 , CCIR493-4 ,CROWD36 (partially) and CIS36-50 (BEE) but more modes will be added soon.
The program can decode directly from your PCs soundcard or from a WAV file.

The program is written in Java so it should run on any PC which has Java installed.
If you haven't got Java then you can download it for free from ..

http://www.java.com

This program uses the excellent JTransforms library ..

http://sites.google.com/site/piotrwendykier/software/jtransforms

The main problem users have reported when trying to run Rivet under MS Windows is that a program other than Java has taken
ownership of the .JAR extension. If you have this problem try running this program ..

http://johann.loefflmann.net/en/software/jarfix/index.html

(Thanks to Mario for that)

Change Log
----------

Build 04 adds an online help facility and XPA2 decoding.
Build 05 adds the foundations of CROWD36 decoding and hopefully fixes the logging bug.
Build 06 added basic CROWD36 decoding
Build 07 allows direct sound card decoding.
Build 08 fixed a bug which caused the program to read from the sound card after a WAV file had been loaded.
         also improved XPA and XPA2 symbol timing and support for 20 baud XPA.
Build 09 improves CROWD36 decoding and adds the copy to clipboard feature.
Build 10 adds basic binary CIS 36-50 decoding and the option to define the CROWD36 high sync tone in use.
Build 11 improves the display of CIS 36-50 messages.
Build 12 adds basic FSK200/500 decoding and improved decoding of CIS 36-50
Build 13 allows settings to be saved and then reloaded at startup
Build 14 auto detects if the signal needs to be inverted when decoding XPA2.
Build 15 detects the start and end of CIS36-50 messages.
Build 16 displays CIS36-50 traffic in 7 bit blocks.
Build 17 incorporates information received on the make up of CIS36-50 messages
Build 18 adds an early/late gate to improve CIS36-50 symbol sync
Build 19 further improves the CIS36-50 early/late gate and improves FSK200/500 decoding
Build 20 much better decoding of CIS36-50 and FSK200/500.
Build 21 added experimental CCIR493-4 decoding
Build 22 yet more experimental CCIR493-4 decoding features
Build 23 adds CCIR493-4 decoding
Build 24 fixes a bug in CCIR493-4 decoding putting leading zeros in front of station identity sections
Build 25 adds basic error correction to the CCIR493-4 mode 
Build 26 adds a input level slider plus other improvements to the look of the status bar also improve the FSK200/500 code. 
Build 27 adds further CCIR493-4 debugging data to try and find out why weak signals are being lost
Build 28 hopefully further improve the detection of weak CCIR493-4 signals
Build 29 improved FSK200/1000 decoding
Build 30 adds proportional early/late gate control to the FSK modes.
Build 31 Improved input level control which can attenuate,improved CIS36-50 decoding,FSK200/500 status bar updating,
		 improved FSK200/1000 binary decoding
Build 32 adds basic FSK200/1000 frame decoding
Build 33 fixes an ITA3 character set bug , checks for 8 bit WAV files and enables bitstream outputs to be saved
Build 34 Inverts the FSK200/1000 bit demodulation and extracts the block number from a block. Also add very basic
         GW 100 baud FSK support.
Build 35 Fixes a bug which calculated the total number of FSK200/1000 blocks in a message.
Build 36 Adds baudot decoding and limits bitstream out lines to 80 characters.


Reported Bugs
-------------

Still problems with CROWD36 decoding. Not a bug as such but more a lack of understanding of this mode.

Also while the program decodes CIS36-50 messages OK from stations that start transmitting idle briefly but
has problems with messages from stations which idle constantly.

Ian Wraith (20th October 2012) 