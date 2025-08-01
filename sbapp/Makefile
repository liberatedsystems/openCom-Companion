all: prepare debug

prepare: cleanrns getrns

clean:
	@echo Cleaning...
	-(rm ./__pycache__ -r)
	-(rm ./app_storage -r)
	-(rm ./share/pkg/* -r)
	-(rm ./share/mirrors/* -rf)
	-(rm ./bin -r)

cleanlibs:
	. venv/bin/activate; buildozer android clean
	
cleanall: clean cleanlibs

pacthfiles: patchsdl injectxml patchpycodec2

patchsdl:
	# Pach USB HID behaviour
	cp patches/HIDDeviceUSB.java .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/build/bootstrap_builds/sdl2/jni/SDL/android-project/app/src/main/java/org/libsdl/app/HIDDeviceUSB.java
	cp patches/HIDDeviceUSB.java .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/src/main/java/org/libsdl/app/HIDDeviceUSB.java
	cp patches/HIDDeviceUSB.java .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/jni/SDL/android-project/app/src/main/java/org/libsdl/app/HIDDeviceUSB.java

	# Pach service loader
	cp patches/PythonService.java .buildozer/android/platform/python-for-android/pythonforandroid/bootstraps/common/build/src/main/java/org/kivy/android/PythonService.java
	cp patches/PythonService.java .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/build/bootstrap_builds/sdl2/src/main/java/org/kivy/android/PythonService.java
	cp patches/PythonService.java .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/src/main/java/org/kivy/android/PythonService.java

patchpycodec2:
	patchelf --replace-needed libcodec2.so.1.2 libcodec2.so .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/_python_bundle__arm64-v8a/_python_bundle/site-packages/pycodec2/pycodec2.so
	patchelf --replace-needed libcodec2.so.1.2 libcodec2.so .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/_python_bundle__armeabi-v7a/_python_bundle/site-packages/pycodec2/pycodec2.so

injectxml:
	# mkdir /home/markqvist/.local/lib/python3.11/site-packages/pythonforandroid/bootstraps/sdl2/build/src/main/xml
	# Inject XML on arm64-v8a
	mkdir -p .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/src/main/res/xml
	mkdir -p .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/templates
	cp patches/device_filter.xml .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/src/main/res/xml/
	cp patches/file_paths.xml .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/src/main/res/xml/
	cp patches/AndroidManifest.tmpl.xml .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/templates/
	cp patches/p4a_build.py .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/build.py
	
debug:
	. venv/bin/activate; buildozer android debug

prebake:
ifneq (,$(wildcard .buildozer/android/platform/build-arm64-v8a_armeabi-v7a/dists/occ/src/main/res/xml/device_filter.xml))
	@echo Device filter XML exists, skipping prebake
else
	@(echo Prebaking build before patching files...)
	@(sleep 2)
	-(. venv/bin/activate; buildozer android release)
	@(echo Prebake finished, applying patches and rebuilding...)
	@(sleep 2)
endif

fetchshare:
	-(rm ./share/pkg/*)
	-(rm ./share/mirrors/* -r)
	if [ ! -d "./share/pkg" ]; then mkdir ./share/pkg; fi
	if [ ! -d "./share/mirrors" ]; then mkdir ./share/mirrors; fi
	cp ../../dist_archive/rns-*-py3-none-any.whl ./share/pkg/
	cp ../../dist_archive/rnspure-*-py3-none-any.whl ./share/pkg/
	cp ../../dist_archive/lxmf-*-py3-none-any.whl ./share/pkg/
	cp ../../dist_archive/nomadnet-*-py3-none-any.whl ./share/pkg/
	cp ../../dist_archive/rnsh-*-py3-none-any.whl ./share/pkg/
	cp ../../dist_archive/RNode_Firmware_*_Source.zip ./share/pkg/
	zip --junk-paths ./share/pkg/example_plugins.zip ../docs/example_plugins/*.py
	cp -r ../../dist_archive/reticulum.network ./share/mirrors/
	cp -r ../../dist_archive/unsigned.io ./share/mirrors/
	cp ../../dist_archive/Reticulum\ Manual.pdf ./share/mirrors/Reticulum_Manual.pdf
	cp ../../dist_archive/Reticulum\ Manual.epub ./share/mirrors/Reticulum_Manual.epub
	cp -r ../../rnode-flasher ./share/mirrors/
	-(rm ./share/mirrors/rnode-flasher/.git -rf)

release:
	. venv/bin/activate; buildozer android release

postbuild:
	$(MAKE) cleanrns

apk: prepare prebake pacthfiles fetchshare release postbuild

devapk: prepare prebake pacthfiles fetchshare debug postbuild

version:
	@(echo $$(python ./gv.py))

install:
	adb install bin/occ-$$(python ./gv.py)-arm64-v8a_armeabi-v7a-release.apk

console:
	(adb logcat | grep "python\|occservice")

getrns:
	(cp -rv ../../Reticulum/RNS ./)
	-(rm ./RNS/Utilities/RNS)
	-(rm ./RNS/__pycache__ -r)
	(cp -rv ../../LXMF/LXMF ./)
	-(rm ./LXMF/Utilities/LXMF)
	-(rm ./LXMF/__pycache__ -r)
	(cp -rv ../../LXST/LXST ./)
	-(rm ./LXST/Utilities/LXST)
	-(rm ./LXST/__pycache__ -r)
	-(rm ./LXST/Utilities/__pycache__ -r)

cleanrns:
	-(rm ./RNS -r)
	-(rm ./LXMF -r)
	-(rm ./LXST -r)
