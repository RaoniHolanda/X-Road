# automatic backups once a day
15 3 * * * xroad /usr/share/xroad/scripts/autobackup_xroad_center_configuration.sh

# backup retention policy, delete backups older that 30 days
10 * * * * xroad find /var/lib/xroad/backup -type f -name "cs-automatic-backup*.gpg" -mtime 30 -delete

# reload nginx, if configuration https certificates have changed
*/5 * * * * xroad /usr/share/xroad/scripts/reload_nginx_if_global_conf_cert_changed.sh
