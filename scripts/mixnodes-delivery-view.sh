echo "scenario mix created delivered" #> ../../../articles/opportunistic-anonymous-messaging/graph/data/histr.data
for a in  info5 taxis cambridge festi mit cisco asturies
do 
for mix in `seq 1 5`
do
file=reports/mix-$a-$mix/*MessageStatsReportPerApplication.txt
string=`cat $file 2>/dev/null |grep "for app"|sort -g |tail -1|awk '{print $NF}'`
created=`grep created $file 2>/dev/null |head -1|cut -d" " -f2`
delivered=`cat $file 2>/dev/null |grep -A8 "$string"|grep delivered|cut -d" " -f2`
echo $a $mix $created $delivered |sed -e 's/info5/Info5/g' |sed -e 's/cambridge/Cambridge/g' |sed -e 's/adhoc/AdHoc/g' |sed -e 's/cisco/Cisco/g' |sed -e 's/asturies/Asturies/g' | sed -e 's/mit/MIT/g'
done
done
