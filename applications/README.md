# Notes

You may see the following errors when you open Hadoop in intellij

- You need to: 
  - Use corretto-11 JDK
  - [Disable release option](https://youtrack.jetbrains.com/issue/IDEA-201168)
  - Run `mvn package -DskilTests` to generate generated sources.


- HDFS may crash non-determinismly.
