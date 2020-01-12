echo "value scenario mix" > data/histr.data #> ../../../articles/opportunistic-anonymous-messaging/graph/data/histr.data
for a in  info5 taxis cambridge festi mit cisco asturies
do 
for mix in `seq 1 5`
do
#file=reports/mix-$a-$mix/*MessageDetailedReport.txt
file=reports_aleix/mix-$a-$mix/*MessageDetailedReport.txt
#ls -la $file 2>/dev/null
IFS="
"
for line in `cat $file 2>/dev/null|grep -v Mess|grep -v sim 2>/dev/null`
do
echo $line $mix >> data/histr.data
done
done
done

