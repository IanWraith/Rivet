---------------------------------------------------------

Linux version. Ubuntu 10.04 LTS - le Lynx Lucide


INSTALLING


To make Rivet more convenient to start, you can put a launching script
in /usr/bin.  
#First create a folder "rivet" in you "home" and put the rivet.jar inside.
#Secondly,to install it do the following from the shell:

  cd <rivet_install_root>
sudo nano rivet.sh
copy the script below inside the file:
java -jar /home/Name_of_your_personal_home/Rivet/rivet_bXX.jar

cd $usr/bin
./rivet

CTRL X and save it

#Thirdly copy the file and modify the right user.
sudo cp rivet.sh /usr/bin/rivet
sudo chmod 755 /usr/bin/rivet


Now you can generate the launcher in the main menu.
Fill the past in the new element.

---------------------------------------------------------

My thanks to user Laurent for this information.
