#!/bin/bash

if $(git -C . rev-parse &> /dev/null); then
  synced_to_cl=@$(git5 status --change)
else
  synced_to_cl=@$(srcfs get_readonly)
fi

repo_dir=$(p4 g4d -f j2cl-size)
cd $repo_dir
g4 sync $synced_to_cl
