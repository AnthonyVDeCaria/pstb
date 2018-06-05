import sys
import numpy as np
import matplotlib.pyplot as plt
from scipy.optimize import curve_fit

def fit_func(x, a, b, c):
     y = a * np.exp(-b * x) + c
     return y

# 0->pythonFile 1->folderPath 2->title 3->xLabel 4->yLabel 5->xData 6->yData 7->graphLabels

numArgs = len(sys.argv)
if numArgs != 8:
    print("numArgs Error!")
    sys.exit(10) # numArgs

folderPath = sys.argv[1]
titl = sys.argv[2]
xLab = sys.argv[3]
yLab = sys.argv[4]
givenXs = sys.argv[5].split("|")
givenYs = sys.argv[6].split("|")
graphLabels = sys.argv[7].split("|")

xs = []
ys = []
ls = []

plt.figure(figsize=(16, 9), dpi=120)

for graphXI in givenXs:
    brokenGraphXI = graphXI.split("-")
    xI = []
    for valueXJ in brokenGraphXI:
        temp = valueXJ.replace("[","").replace("]","")
        xI.append(float(temp))
    xs.append(xI)

for graphYI in givenYs:
    brokenGraphYI = graphYI.split("-")
    yI = []
    for valueYJ in brokenGraphYI:
        temp = valueYJ.replace("[","").replace("]","")
        yI.append(float(temp))
    ys.append(yI)

colours = plt.cm.jet(np.linspace(0, 1, len(xs)))
for xI, yI, labelI, colourI in zip(xs, ys, graphLabels, colours):
    plt.plot(xI, yI, ".", label=labelI, color=colourI)
    plt.plot(np.unique(xI), np.poly1d(np.polyfit(xI, yI, 3))(np.unique(xI)), color=colourI) 

   
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
plt.legend()
plt.savefig(folderPath+titl+'.png', bbox_inches='tight')

