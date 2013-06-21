# You need wget for this to work properly. If you don't already have wget (and you're using a MS Windows-based platform), you
# can download wget from the following link: http://gnuwin32.sourceforge.net/packages/wget.htm
# Run this script from the same directory wget.exe gets installed to.

wget --no-check-certificate "https://tts.nuancemobility.net:443/NMDPTTSCmdServlet/tts?codec=wav&ttsLang=en_US&text=Hello world. This is a greeting from Nuance.&appId=[INSERT YOUR APP ID]&appKey=[INSERT YOUR 128-BYTE STRING APP KEY]&id=0000" -O sample_audio_lang.wav -o test_results.log