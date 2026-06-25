# How To Download
Either clone the project and build by yourself using compile.sh script or download the [File.zip](https://github.com/Zydak/graphwar-Mod/releases/download/1.0/Files.zip) inside [releases](https://github.com/Zydak/graphwar-Mod/releases/tag/1.0), unzip, and run with `java -jar graphwar.jar`.

# Mod Menu Overview

## Graph Plane

### Function Preview
You can input your function inside the mod menu which will draw a function preview. This way you can see whether your function will hit the target and adjust the formula if needed. And after you're done inputting your function you can send it to the game automatically with SEND button, no need to copy paste. Also note that for the preview to work properly you have to first select the soldier you want to shoot with.

https://github.com/user-attachments/assets/7c22ef8c-81cd-426e-9788-49fab87fbffd

---
### Shortened Notation
Another thing is support for shortened notation for common useful functions, for example spawning sin after certain x. So instead of typing `2sin(5x)/(1+exp(-10*(x-5)))` you can do `spsin(2, 5, 5)`.

Currently supposed notations are:
* `spsin(height, frequency, startPoint)` = `height*sin(frequency*x)/(1+exp(-100*(x-startPoint)))`
* `step(height, startPoint)` = `height/(1+exp(-100*(x-startPoint)))`

https://github.com/user-attachments/assets/0538ccaa-49d4-4777-9a68-fc39f20891c0

https://github.com/user-attachments/assets/7ee69186-8602-47fc-bd41-45d5b39cfa6a





## Graph Formula Generator

Mod menu has functionality to automatically generate a math formula for intersecting as many given points as possible. It automatically fetches player positions form the game and generates a formula. 

It works by sorting targets left to right (since a function can only go one direction on the x axis), then uses A* pathfinding to route around obstacles between each pair of targets. The resulting path is simplified to as few waypoints as possible, then converted into a formula using the `abs(x-a) - abs(x-b)` trick to activate each segment over the right x range.

```java
String formula = GraphFormulaGenerator.generateFormula(waypoints, obstacles);
```

You can also make you own path by selecting draw mode and clicking position manually

https://github.com/user-attachments/assets/855dba81-4306-4fae-a46b-0f6ad22b4054

https://github.com/user-attachments/assets/83672f8f-fddd-42de-b755-a122dce3b024

https://github.com/user-attachments/assets/ed5e4a74-2cc1-402e-b1bc-5a5275337b65



---
# ORIGINAL README
---

# Graphwar Tutorial

Graphwar is an artillery game in which you must hit your enemies using mathematical functions. The trajectory of your shot is determined by the function you wrote, and your goal is to avoid the obstacles and your teammates and hit your enemies. The game takes place in a Cartesian Plane.

![cam](/../screenshots/ss1graphwar.png?raw=true)

## Game Modes

## Normal Function 

The Normal Function mode is the most basic mode. In this mode the function shot is simply the function you typed in, so the trajectory of your shot will be same trajectory as the function's graph. 
However, there is a problem. The function must be shot by your soldier, but there's is no guarantee that the point where your soldier is standing belongs to the function. To solve this the function must be translated until the position of the soldier is part of the function, this is done adding a constant to the function. That means that if a function y = f(x) is typed the actual graph is actually going to be y = f(x)+c. 


## First Order Differential Equation

In this mode you enter a first order differential equation instead of a function. For example:

* y' = 3*sin(x)+2
* y' = -y/3
* y' = 1/(x+y)

On this mode no constant is added to your function. Instead your soldier position is used as the initial condition to solve the differential equation and the graph fired is the actual solution.


## Second Order Differential Equation

The second order differential equation mode is very similar to the first order mode, but now you enter a second order differential equation:

* y'' = -y + y' + 2*x - 1
* y'' = 4*sin(x) + 2^x
* y'' = 1.04^(-(x+ y)^2)

To have a unique solution, a second order differential equation must have two initial conditions, the first is the soldier's position and the second is the firing angle. You can change the firing angle by pressing up and down on the keyboard. Also note that this is the only mode that the angle affects the function.


## Common Pitfalls

![cam](/../screenshots/ss2Graphwar.png?raw=true)

The translation of the function have some confusing consequences. First, any constant added to your function is irrelevant to the result. For example, the functions y = 2*x + 3, y = 2*x - 8 and y = 2*x yield the exact same graph in the game.

Other confusing fact is related to the fact that the x axis limits on the game are -25 and +25 and the y axis limits are -15 and 15. That means functions can get very big. For example the function y = x^2 has the value 100 when x equals 10. That means this function will hit the ceiling of the game very fast. If your soldier is positioned on a position where x is -15 this function will be very very steep, it will most likely appear as a straight line up or down. Remember that a huge constant will have to be added to this function, so the result is something very different from what you might be expecting. This problem can be solved by scaling the function appropriately, the function y = (x^2)/50 will produce a nice looking parabola.

Another thing that may confuse you is that your soldiers will always be standing on negative values for x. Your team is located to the left of the y axis, so that is expected and it means functions like y = sqrt(x) will not like you and will explode immediately. You should try something like y = sqrt(abs(x)). 
As was just pointed out functions may explode spontaneously. That means it had an invalid value at that point, a square root of a negative number or a function that gets vertical at a point will explode. Another possible reason for a function to explode is that it is too long, a sine with a high frequency may reach the maximum function length allowed and spontaneously explode.


## Function Syntax

### Variables

* x
* y
* y'

### Operators

* \+
* \-
* /
* \*
* ^

### Functions

* sqrt()
* log()
* ln()
* abs()
* sin()
* cos()
* tan()
* exp()

### Other Examples

* y = ((x-3)^2)/20
* y = ln(abs(x))
* y = sin(x/20)*5
* y' = 1.2^x
* y'' = (1.2^(-(x+3)^2))*(20*(-y))

Using lots of parentheses is recommended to avoid misinterpretation, for example y = 1/x+2 is going to be understood as (1/x) + 2, you should use 1/(x+2).


### Chat Commands

The available commands are:

* -skip : If everyone playing uses this command, the current map is skipped and a new one is generated.
* -sayfunc : If you use this you are going to see on your chat the function that everyone else is using.
* -stopsayfunc : This will stop the functions from appearing on your chat after you used -sayfunc.
* -shownext : This will highlight the next soldier to play for each player with a dark circle. This is useful to plan functions ahead of time.
* -stopshownext : Stops showing the next soldier to play.

Just type them on the game chat to use them.

## Running The Game

Compile the game using the make command (or on your favorite IDE).

To run the game execute graphwar.jar.

## Running Local Servers

To run a local server and connect to it you must pass the ip of the local server to graphwar
and to the globalServer as a command line argument. So to start a server locally the commands are:

* java -jar globalServer.jar [your-server-ip]
* java -jar roomServer.jar [your-server-ip]
* java -jar graphwar.jar [your-server-ip]
