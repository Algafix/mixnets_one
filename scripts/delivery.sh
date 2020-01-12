echo "Scenario Created Delivered"
for scen in Info5 Cambridge Taxis Demo Festival
do
created=`grep created reports/default/$scen*MessageStatsReportPerApplication.txt |head -1|cut -d" " -f2`
delivered=`cat reports/default/$scen*MessageStatsReportPerApplication.txt |grep -A8 "r-r-r-influ"|grep delivered|cut -d" " -f2`
echo "$scen $created $delivered"
done
