echo "value scenario mix" > data/histrLatencyMixNodes.data 
for a in MixnetEpidemicRouter MixnetSnWRouter
do 
for mix in 0 3 6 9
do
#file=reports/mix-$a-$mix/*MessageDetailedReport.txt
file=reports_sim/LatencyMixNodes/$a/*$mix\_MessageDetailedReport.txt
#ls -la $file 2>/dev/null
IFS="
"
for line in `cat $file 2>/dev/null|grep -v Mess|grep -v sim 2>/dev/null`
do
echo $line $mix >> data/histrLatencyMixNodes.data
done
done
done

