# DemoFFMPEG

A simple work arround to compress video file

following commmand is used for this particular compressions.

new String[] { "/data/data/"+packageName+"/ffmpeg",
                "-i",
                inputFile, "-s","480x320","-acodec","mp2",
                "-strict","-2","-ac","1","-ar","16000","-r","13","-ab","32000",
                "-aspect","3:2",outputFile};
                


<p align="center">
  <img src="https://github.com/amitrai98/DemoFFMPEG/blob/master/app/compression.gif?raw=true" width="350"/>
</p>
