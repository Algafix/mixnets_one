echo "value scenario mix" > data/histrAleix.data #> ../../../articles/opportunistic-anonymous-messaging/graph/data/histr.data
for a in testAleixB #info5 taxis cambridge festi mit cisco asturies
do 
#for mix in `seq 1 5`
#do
#file=reports/mix-$a-$mix/*MessageDetailedReport.txt
file=reports_aleix/$a/*MessageDetailedReport.txt
#ls -la $file 2>/dev/null
IFS="
"
for line in `cat $file 2>/dev/null|grep -v Mess|grep -v sim 2>/dev/null`
do
#echo $line $mix >> data/histr.data
echo $line 1 >> data/histrAleix.data
done
#done
done

