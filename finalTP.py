import sys
import re
import numpy as np
import matplotlib.pyplot as plt
from scipy.optimize import curve_fit

# 0->pythonFile 1->folderPath 2->title 3->yLabel 4->labels 5->data 6->NameRegex

numArgs = len(sys.argv)
if numArgs != 7:
    print("numArgs Error!")
    sys.exit(10) # numArgs

folderPath = sys.argv[1]
titl = sys.argv[2]
yLab = sys.argv[3]
givenLabels = sys.argv[4].split("|")
givenData = sys.argv[5].split("|")
regex = sys.argv[6]

tpData = {}

fig = plt.figure(figsize=(16, 9), dpi=120)
ax = fig.add_axes([0,0,1,1])

label0 = givenLabels[0]
brokenL0 = label0.split(",")
multipleRuns = bool(re.search(regex, brokenL0[0]))

for labelI, dataI in zip(givenLabels, givenData):
    temp = np.array(float(dataI.replace("[","").replace("]","")))
    
    isFound = False
    keyI = ""
    brokenLI = labelI.split(",")
    
    if multipleRuns == True:
        for key in tpData.keys():
            brokenKey = key.split(",")
            test = set(brokenKey).intersection(brokenLI)
            
            if(len(test) == len(brokenKey)):
                isFound = True
                keyI = key
                break
        if isFound == False:
            brokenLI.pop(0)
            keyI = ','.join(str(b) for b in brokenLI)
            tpData[keyI] = temp
        else:
            existingData = tpData.pop(keyI)
            existingData = np.append(existingData, temp)
            tpData[keyI] = existingData
    else:
        tpData[labelI] = temp

tickNames = []
ft = []
error = []
for d in tpData.items():
    lI = d[0]
    dI = d[1]
    tickNames.append(lI)
    ft.append(np.mean(dI))
    error.append(np.std(dI))

x_pos = np.arange(len(tickNames))
ax.bar(x_pos, ft, yerr=error)
ax.set_xticks(x_pos)
ax.set_xticklabels(tickNames, rotation=90)
ax.set_title(titl)
ax.set_ylabel(yLab)

plt.savefig(folderPath+titl+'.png', bbox_inches='tight')

