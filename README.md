# gitlab-backup

docker run -it --rm -e URL_GITLAB=https://url-gitlab -e USUARIO=usuario -e SENHA=senha -e BACKUP_PATH=/backup -v /gitlab-backup:/backup seniocaires/gitlab-backup