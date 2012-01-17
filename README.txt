Rivet is a free open source decoder of various HF data modes which interest members 
of the Enigma 2000 group.

http://groups.yahoo.com/group/enigma2000/

Currently the program decodes the modes XPA , XPA2 , CROWD36 (partially) and CIS36-50 (BEE) but more modes will be added soon.
The program can decode directly from your PCs soundcard or from a WAV file (mono and using a sample rate of
11.025 KHz or 8 KHz).

The program is written in Java so it should run on any PC which has Java installed.
If you haven't got Java then you can download it for free from ..

http://www.java.com

This program uses the excellent JTransforms library ..

http://sites.google.com/site/piotrwendykier/software/jtransforms

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

Reported Bugs
-------------

Still problems with CROWD36 decoding. Not a bug as such but more a lack of understanding of this mode.

Ian Wraith (17th January 2011) 