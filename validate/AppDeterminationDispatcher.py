# -*- coding: utf-8 -*-
#    A validation script for Bonnmotion (http://net.cs.uni-bonn.de/wg/cs/applications/bonnmotion/)
#    Copyright (C) 2011 University of Bonn
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

from Config import Config
from DataAccess import DataAccess
from Common import Hashes, runBonnmotionApp

import threading, os, getpass, datetime

class AppDeterminationDispatcher(object):
    def __init__(self, n, params):
        dict = {}
        self.threads = []                            #sequence of threads
        noofthreads = Config().readConfigEntry('noofthreads')
        
        for i in range(noofthreads):
            dict[i] = []
        
        for i in range(n):
            y = i % noofthreads
            dict[y].append(i)
            
        for i in range(noofthreads):
            thread = self.AppDeterminationThread(dict[i], params)      #create thread
            self.threads.append(thread)
            thread.start()
        
        for t in self.threads:
            t.join()                            #wait for threads to finish
            
    #######################################
    #deletes all unneeded files created
    def cleanup(self):
        for t in self.threads:
            for i in t.Seq:
                try:
                    os.remove(os.path.join(Config().readConfigEntry('bonnmotionpath'), Config().readConfigEntry('tempoutputmovementsfile').replace('INDEX', str(i))))
                    os.remove(os.path.join(Config().readConfigEntry('bonnmotionpath'), Config().readConfigEntry('tempoutputparamsfile').replace('INDEX', str(i))))
                    os.remove(Config().readConfigEntry('tempoutputparamsfile').replace('INDEX', str(i)))   
                    for case in t.Cases:
                        for x in case['extensions']: 
                            os.remove(os.path.join(Config().readConfigEntry('bonnmotionpath'), Config().readConfigEntry('tempoutputname') + str(i)) + '.' + x)
                except OSError: pass
                 
    class AppDeterminationThread(threading.Thread):   
        def __init__(self, seq, params):
            threading.Thread.__init__(self)
            self.App = params['app']
            self.Seq = seq                          #numbers of outputfiles to run with BM
            self.Cases = params['cases']
        def run(self):
            for i in self.Seq:
                for case in self.Cases:
                    paramsfilename = os.path.join(Config().readConfigEntry('bonnmotionpath'), Config().readConfigEntry('tempoutputparamsfile').replace('INDEX', str(i)))
                    movementsfilename = os.path.join(Config().readConfigEntry('bonnmotionpath'), Config().readConfigEntry('tempoutputmovementsfile').replace('INDEX', str(i)))
                    outputfilename = os.path.join(Config().readConfigEntry('bonnmotionpath'), Config().readConfigEntry('tempoutputname') + str(i))    

                    if 'appparams' in case:
                        runBonnmotionApp(Config().readConfigEntry('bonnmotionpath'), i, self.App, case['appparams'])
                    else:
                        runBonnmotionApp(Config().readConfigEntry('bonnmotionpath'), i, self.App, '')
                    ordering = []
                    content = ''
                    for ext in case['extensions']:
                        ordering.append(ext)
                        #open file
                        f = open(outputfilename + '.' + ext)
                        content = content + f.read()
                        f.close()    

                    #read parameters
                    f2 = open(paramsfilename)
                    params = f2.read()
                    f2.close()

                    p = {}
                    if 'appparams' in case:
                        p['appparameters'] = case['appparams']
                    else:
                        p['appparameters'] = ''
                    p['identifier'] = self.App
                    p['bmparamsfile'] = params
                    Hashes().calcHashes(p, content)
                    p['user'] = getpass.getuser()
                    p['datetime'] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")

                    tmp = ''
                    for y in ordering:
                        tmp = tmp + y + ','
                    p['ordering'] = tmp[0:-1]

                    #save in DB
                    DataAccess().save(p)  