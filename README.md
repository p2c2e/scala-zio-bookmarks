# Verify your bookmarks using Scala / ZIO
## How to build the code
0. Ensure you have Scala 2.13+ available
1. Clone the code
2. Build the code 
    
        sbt build
1. Create the Uber-jar to run 

        sbt assembly
1. Run it passing the bookmark filename. There is a *sample.html* file if you want to try it out.
    
        scala -jar ./target/scala-2.13/scala-zio-bookmarks-assembly-0.1.jar mybookmarks.html
    The output of the run will result in the &lt;orig_name&gt;.out file
    
## Backstory 
I have around 4000+ bookmarks collected over many years.
I am pretty sure, I will not be visiting most of them - and most likely many of these website do not exist anymore...

To validate this hypothesis (and as an excuse to write some code), I wrote this app.

## What is interesting
* The Bookmarks HTML format has been around for decades. It was invented by Nescape in early Internet years and is not well-formed HTML
* Original attempt was to use JSoup library to parse and modify the HTML. This did not work - since Chrome (which I used to test), does not accept wellformed HTML - and usually silently ignores chunks of content.
* There are very-few libraries to parse the bookmarks file format - and none in Java AFAIK
* Since the work is network intensive and not CPU, the thought was to parallelize and speed up the checks

## Benefits of ZIO
* Going from sequential to parallel is only few chars of change
* The 'underlying code' is normal code that gets wrapped in ZIO
* While writing the code, it is clear what errors can crop up based on strong typing

