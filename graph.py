import sys
import numpy as np
import matplotlib.pyplot as plt

# 0->pythonFile 1->AOType 2->folderPath 3->title 4->xLabel 5->yLabel 6->xData 7->xType 8->yData 9->Type

numArgs = len(sys.argv)
if numArgs != 10:
    sys.exit(10) # numArgs

aoType = sys.argv[1]
folderPath = sys.argv[2]
titl = sys.argv[3]
xLab = sys.argv[4]
yLab = sys.argv[5]
givenXs = sys.argv[6].split(",")
xType = sys.argv[7]
givenYs = sys.argv[8].split(",")
yType = sys.argv[9]

x = []
y = []

plt.figure(figsize=(16, 9), dpi=90)

if xType == "long":
    for xI in givenXs:
        x.append(long(xI.replace("[","").replace("]","")))
elif xType == "float":
    for xI in givenXs:
        x.append(float(xI.replace("[","").replace("]","")))
else:
    for xI in givenXs:
        x.append(xI.replace("[","").replace("]",""))
        
if yType == "long":
    for yI in givenYs:
        y.append(long(yI.replace("[","").replace("]","")))
elif yType == "float":
    for yI in givenYs:
        y.append(float(yI.replace("[","").replace("]","")))
else:
    for yI in givenYs:
        y.append(yI.replace("[","").replace("]",""))

print("Printing '"+titl+"' to file...")
if aoType == "delayCounter":
    plt.plot(x, y, ".")
elif aoType == "histogram":
    plt.plot(y, "-o")
    plt.xticks(np.arange(len(x)), x, size='small')
elif aoType == "throughput":
    plt.plot(x, y, ".")
    plt.plot(np.unique(x), np.poly1d(np.polyfit(x, y, 1))(np.unique(x)))
else:
    sys.exit(11) #Bad_Args

plt.title(titl)
plt.xlabel(xLab)
plt.ylabel(yLab)
plt.savefig(folderPath+titl+'.png', bbox_inches='tight')
