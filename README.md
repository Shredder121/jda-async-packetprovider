# jda-async-packetprovider
JDA PacketProvider that keeps a backlog of audio packets

## How to get

```xml
<dependencies>
	<dependency>
		<groupId>com.github.shredder121</groupId>
		<artifactId>jda-async-packetprovider</artifactId>
		<version>1.3.1-MINN</version>
	</dependency>
</dependencies>
```

You can also use the gradle equivalent of declaring the dependency.


## How to use

During construction of a JDA instance, using a JDABuilder, you can use the `setAudioSendFactory` to set an audio send factory.
In the case of the `AsyncPacketProviderFactory` you'll have to wrap the original audio sender in the adapted one.

```java
JDABuilder builder = //...

builder.setAudioSendFactory(
		AsyncPacketProviderFactory.adapt(/*original audio sender*/)
);
```

I can recommend the [NativeAudioSender][jda-nas], as its buffering and sending packets off-heap greatly reduces stuttering caused by GC pauses.


[jda-nas]: https://github.com/sedmelluq/jda-nas
