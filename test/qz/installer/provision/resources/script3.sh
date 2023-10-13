shell=$(ps -p $$ -oargs=|awk '{print $1}')
date=$(date "+%F %T")
script=$(basename "$0")
user="$(eval echo ~$(logname))"

echo "$date Successful provisioning test from '$shell': $script" >> "$user/Desktop/provision.log"
chmod 555 "$user/Desktop/provision.log"