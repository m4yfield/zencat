/*
    zencat - a zenoss irc bot
    Copyright (C) 2012 Katherine Daniels <katherine.daniels@livestream.com>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; version 2 of the GPL only, not 3 :P

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.ls.zencat;

import java.net.*;
import java.io.*; 

// passes command to external program and returns results back to irc
class CatHandler  extends Thread {
        
        ZenCat bot;
        Socket sock;

        CatHandler(Socket s, ZenCat b){
            sock = s;
            bot = b;
        }
        
        public void run(){
            try{
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                        sock.getInputStream(), "UTF-8"));
                String inputLine = new String();
                String recipients[] = null; 
                boolean all = false;
                int i = 0;
                while ((inputLine = in.readLine()) != null) {
                	if(i++==0){
                		String[] words = inputLine.split(" ");
                		if(words[0].equals("#*")){
                			// send to all channels
                			all = true;
                			inputLine = inputLine.substring(3);
                		}else
                		if(words[0].startsWith("#") || words[0].startsWith("@")){
                			String addressees[] = words[0].split(",");
                			for(int j=0; j<addressees.length; ++j){
                				if(addressees[j].startsWith("@")){
                					// to a user, strip the @ for
									// sendMessage()..
                					addressees[j] = addressees[j].substring(1); 
                				}
                			}
                			recipients = addressees;
                			inputLine = inputLine.substring(words[0].length()+1);
                		}else{
                			// nothing specified. use default channel from
							// config.
                			recipients = new String[1];
                			recipients[0] =  bot.getDefaultChannel() ;
                		}
                	}
                    
                	// now send it to the recipients:
                	if(all)
                		bot.catStuffToAll(inputLine);
                	else
                		bot.catStuff(inputLine, recipients);

                }
                in.close();
                //System.out.println("Handler finished.");
            }
            catch(Exception e){ e.printStackTrace(); }
        }

}


