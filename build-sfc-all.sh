

pwd
pwd=$(pwd)

# libraries used by other modules, must be build and published first
libraries=("core/sfc-core" "core/sfc-ipc" "adapters/modbus")

for l in "${libraries[@]}";do
    cd "$pwd/$l" || exit
    echo "*** Building and publishing  $l ***"
    gradle build
    gradle publish
    cd "$pwd" || exit
  done


#build build sfc modules
for p in "core" "targets" "adapters" "metrics"; do

    for d in $(ls -d "$p"/*); do

       cd "$pwd/$d" || exit

          for l in "${libraries[@]}";do
               #skip libraries
               if [ "$l" != "$d" ] && [ -f "build.gradle.kts" ];then
                  echo "*** Building $d ***"
                  gradle build
              fi
          done

       cd "$pwd" || exit
  done
done


cd $pwd
