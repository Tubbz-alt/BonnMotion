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
from Common import readModelnameFromParamsFile, runBonnmotionModel

import threading, os
        
class BonnmotionDispatcher(object):  
    def __init__(self, n, path):
        dict = {}
        self.threads = []                            #sequence of threads
        noofthreads = Config().readConfigEntry('noofthreads')
        
        for i in range(noofthreads):
            dict[i] = []

        for i in range(n):
            y = i % noofthreads
            dict[y].append(i)
            
        for i in range(noofthreads):
            thread = self.BonnmotionJobThread(dict[i], path)      #create thread
            self.threads.append(thread)
            thread.start()
        
        for t in self.threads:
            t.join()                            #wait for threads to finish   
    
    def cleanup(self):
        for t in self.threads:
            for i in t.Seq:
                os.remove(os.path.join(t.path, Config().readConfigEntry('tempoutputmovementsfile').replace('INDEX', str(i))))
                os.remove(os.path.join(t.path, Config().readConfigEntry('tempoutputparamsfile').replace('INDEX', str(i))))
                os.remove(Config().readConfigEntry('tempoutputparamsfile').replace('INDEX', str(i))) 
        
    class BonnmotionJobThread(threading.Thread):
        def __init__(self, seq, path):
            threading.Thread.__init__(self)
            self.Seq = seq                          #numbers of outputfiles to run with BM
            self.path = path
        def run(self):
            for i in self.Seq:
                modelname = readModelnameFromParamsFile(Config().readConfigEntry('tempoutputparamsfile').replace('INDEX', str(i)))
                runBonnmotionModel(self.path, i, modelname)    