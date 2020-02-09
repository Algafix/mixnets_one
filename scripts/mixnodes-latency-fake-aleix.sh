echo "value scenario maxTime" > data/histrLatencyFakeMixNodes.data 
for a in MixnetEpidemicRouter MixnetSnWRouter
do 
for mix in 100 500 1000 2000 3000
do
#file=reports/mix-$a-$mix/*MessageDetailedReport.txt
file=reports_sim/LatencyFakeMixNodes/$a/*$mix\_MessageDetailedReport.txt
#ls -la $file 2>/dev/null
IFS="
"
for line in `cat $file 2>/dev/null|grep -v Mess|grep -v sim 2>/dev/null`
do
echo $line $mix >> data/histrLatencyFakeMixNodes.data
done
done
done

