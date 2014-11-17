Urmia.io Compute Engine
=======================

this is mostly an independent open-source clone of excellent [Joyent Manta](https://www.joyent.com/products/object-storage)
 compute platform and supports docker images for processing. 

see [Manta](https://apidocs.joyent.com/manta/) on how to interact with it.

Urmia.io is developed in Java but does all disk/net operations in kernel landscape and NIO thanks to [Netty](http://netty.io).

Urmio.io can run on both Linux and Illumos. Ideal environment for Illumos is SDC/SmartOS. for Linux CoreOS/Debian/docker.  

Build Status
============
[ ![Codeship Status for abbaspour/urmia](https://codeship.com/projects/3ce13770-4c18-0132-a9ef-22f26f7d14f3/status)](https://www.codeship.io/projects/46968)