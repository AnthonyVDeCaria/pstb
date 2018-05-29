import sys
import numpy as np
import matplotlib.pyplot as plt
from scipy.optimize import curve_fit

def fit_func(x, a, b, c):
     y = a * np.exp(-b * x) + c
     return y

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

if xType == "int":
    for xI in givenXs:
        x.append(int(xI.replace("[","").replace("]","")))
elif xType == "float":
    for xI in givenXs:
        x.append(float(xI.replace("[","").replace("]","")))
else:
    for xI in givenXs:
        x.append(xI.replace("[","").replace("]",""))
        
xLen = len(x)
        
if yType == "int":
    for yI in givenYs:
        y.append(int(yI.replace("[","").replace("]","")))
elif yType == "float":
    for yI in givenYs:
        y.append(float(yI.replace("[","").replace("]","")))
else:
    for yI in givenYs:
        y.append(yI.replace("[","").replace("]",""))
        
yLen = len(y)

print("Printing '"+titl+"' to file...")
if aoType == "delayCounter":
    plt.plot(x, y, ".")
elif aoType == "histogram":
    plt.plot(y, "-o")
    plt.xticks(np.arange(xLen), x, size='small')
elif aoType == "throughput":
    plt.plot(x, y, ".")
    plt.plot(np.unique(x), np.poly1d(np.polyfit(x, y, 7))(np.unique(x))) 
else:
    sys.exit(11) #Bad_Args
    
'''
    x1 = x[1]
    xMid = x[int(xLen/2)]
    xLast = x[-1]
    y1 = y[1]
    yMid = y[int(yLen/2)]
    yLast = y[-1]
    
    slope1 = (yMid - y1) / (xMid - x1)
    slope2 = (yLast - yMid) / (xLast - xMid)
    
    xNP = np.array(x)
    yNP = np.array(y)
    
    minX = np.amin(xNP)
    maxX = np.amax(xNP)
    minY = np.amin(yNP)
    maxY = np.amax(yNP)
    
    if slope1 > 0:
        if np.abs(slope1) < np.abs(slope2):
            print("a")
            fitting_parameters, covariance = curve_fit(fit_func, xNP, yNP, 
                                                       bounds=(-maxY, 
                                                               maxY))
        else:
            print("b")
            fitting_parameters, covariance = curve_fit(fit_func, xNP, yNP, 
                                                       bounds=(-maxY, 
                                                               maxY))
    else:
        if np.abs(slope1) < np.abs(slope2):
            print("c")
            fitting_parameters, covariance = curve_fit(fit_func, xNP, yNP, 
                                                       bounds=(minY, 
                                                               maxY))
        else:
            print("d")
            fitting_parameters, covariance = curve_fit(fit_func, xNP, yNP, 
                                                       bounds=(minY, 
                                                               maxY))
            
    print("Fit_Param = ", fitting_parameters)
    print("Covariance = ", covariance)
    yFit = fit_func(xNP, *fitting_parameters)
    plt.plot(xNP, yFit, label="Exp")

    plt.plot(np.unique(x), np.poly1d(np.polyfit(x, y, 3))(np.unique(x)), label="x^3")
    
    plt.plot(np.unique(x), np.poly1d(np.polyfit(x, y, 2))(np.unique(x)), label="x^2")
    
    plt.plot(np.unique(x), np.poly1d(np.polyfit(x, y, 4))(np.unique(x)), label="x^4")
'''  

plt.title(titl)
plt.xlabel(xLab)
plt.ylabel(yLab)
plt.savefig(folderPath+titl+'.png', bbox_inches='tight')
