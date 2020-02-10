echo "quantitat tipus protocol temps" > data/histrOverheadFakeMixNodes6.data 
for a in MixnetEpidemicRouter MixnetSnWRouter
do 
for mix in 100 500 1000 2000 3000
do
file=reports_sim/OverheadFakeMixNodes6/$a/*$mix\_MessageQuantityReport.txt
#ls -la $file 2>/dev/null
IFS="
"
for line in `cat $file 2>/dev/null|grep -v Mess|grep -v sim 2>/dev/null`
do
echo $line $mix >> data/histrOverheadFakeMixNodes6.data
done
done
done

