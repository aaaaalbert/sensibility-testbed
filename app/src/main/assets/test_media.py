import androidlog, media, sys, time
l = androidlog.log

# while True:
#	if media.is_media_playing():
#		l("Media is playing")
#	else:
#		l("Media is not playing")
#	time.sleep(1)

path = "/storage/emulated/legacy/Android/data/com.sensibility_testbed/files/"
filename = "recording_" + str(time.time()).replace(".", "") + ".mp4"

l('Lets do the recording')
time.sleep(1)
media.microphone_record(path + filename, 5000)

filename = "recording_" + str(time.time()).replace(".", "") + ".mp4"
l('Lets do some more of that recording')
time.sleep(1)
media.microphone_record(path + filename, 5000)

l('Au revoir')