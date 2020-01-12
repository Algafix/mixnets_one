shuf() { awk 'BEGIN {srand(); OFMT="%.17f"} {print rand(), $0}' "$@" |
               sort -k1,1n | cut -d ' ' -f2-; }

file=$1
msg=msg100
prop=$2

lines=`wc -l $file |cut -d" " -f1`
newlines=`echo "$lines * $prop"|bc -l|cut -d"." -f1`

tmpfile=$(mktemp /tmp/abc-script.XXXXXX)
shuf $file |head -$newlines > $tmpfile

tres=`cat $tmpfile |grep "r2-r1-$msg "|wc -l`
dos=`cat $tmpfile |grep "r1-$msg "|wc -l`
uno=`cat $tmpfile |grep " $msg "|wc -l`

echo $tres $dos $uno

rm $tmpfile
