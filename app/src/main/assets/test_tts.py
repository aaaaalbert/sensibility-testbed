import androidlog, media, sys, time
l = androidlog.log
l('Lets do the media')
text = """Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
tempor incididunt ut labore et dolore magna aliqua."""


def speak(msg):
    l(str(media.tts_speak(msg)))
    while (media.is_tts_speaking()):
        l("is speaking")
        time.sleep(1)

speak(text)
speak("Shake your money make-AH")
speak("Take him to the bridge")

l('KTHXBI')