#while(1){
#  adb shell dumpsys cpuinfo | grep -E "flwr.android_client|TOTAL"
#}
LOG_SUFFIX="220719_2"
TOT_DEVICE_NUM="1"
DATASET="ucihar"
while :
do
  adb shell dumpsys cpuinfo | grep -E "flwr.android_client|TOTAL" >> "./log/realworldFL_${LOG_SUFFIX}_deviceNUM${TOT_DEVICE_NUM}_${DATASET}.txt"
  sleep 60
done
