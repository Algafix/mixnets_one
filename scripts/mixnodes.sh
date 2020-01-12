#Total
export LANG=en_US
delay=500
script=routing
mkdir -p log reports

for mix in `seq 1 5` 
do
for f in info5 taxis cambridge  mit cisco asturies festi
do
file=$f.txt
dir=mix-$f-$mix

if [ ! -d reports/$dir ]; then
      mkdir -p reports/$dir
       out=log/$dir.txt
       cat txt/$file |grep -v "Global.endmessage="|grep -v "Group.router" |grep -v Report.reportDir >$out

echo "Report.reportDir = [ reports/$dir ]"  >> $out
echo "MixnetRouter.nrofmixes = [ $mix ]"  >> $out
echo "Scenario.endTime =  86000" >> $out

echo "./one.sh -b 1 $out > $out.out"
./one.sh -b 1 $out > $out.out  2>&1

fi
done
done
