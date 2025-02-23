devapk:
	make -C sbapp devapk

apk:
	make -C sbapp apk
	mkdir -p ./dist

fetchapk:
	cp ./sbapp/bin/occ-*-release-unsigned.apk ./dist/

install:
	make -C sbapp install

console:
	make -C sbapp console

clean:
	@echo Cleaning...
	-rm -r ./build
	-rm -rf ./dist

cleanbuildozer:
	make -C sbapp cleanall

cleanall: clean cleanbuildozer

preparewheel:
	pyclean .
	$(MAKE) -C sbapp cleanrns

build_wheel:
	. sbapp/venv/bin/activate; python3 setup.py sdist bdist_wheel

build_win_exe:
	python -m PyInstaller sideband.spec --noconfirm

release: build_wheel apk fetchapk

release_docker:
	@echo If you experience errors, please ensure you have docker-buildx installed on your system.
	@echo This target uses a custom buildkit image to allow for full viewing of the logs on build, but takes slightly longer as a result.
	-mkdir -p ./dist
	# Needed for allowing the viewing of the full log
	-sudo docker buildx create --bootstrap --use --name buildkit --driver-opt env.BUILDKIT_STEP_LOG_MAX_SIZE=-1 --driver-opt env.BUILDKIT_STEP_LOG_MAX_SPEED=-1 &> /dev/null
	sudo docker buildx build --target=artifact --output type=local,dest=./dist/ .
	@echo Build successful. APK copied to ./dist directory.
	$(MAKE) sign_release

sign_release:
	@echo This stage generates a keystore in the root directory of the repo and signs the APK with it.
	@echo If a keystore already exists, it is reused.
	@echo Please make sure you have apksigner and zipalign installed on your system before proceeding.
	@echo Press enter to continue.
	@read VOID
	if [ ! -f "./key.keystore" ]; then keytool -genkey -v -keystore key.keystore -keyalg RSA -keysize 4096 -validity 10000 -alias app; fi
	VERSION=$(shell ls ./dist/*.apk | cut -d\- -f 2 | head -n 1); \
	zipalign -p 4 ./dist/occ-*-release-unsigned.apk ./dist/occ-$$VERSION-release.apk; \
	apksigner sign --ks-key-alias app --ks key.keystore ./dist/occ-$$VERSION-release.apk; \

upload:
	@echo Ready to publish release, hit enter to continue
	@read VOID
	@echo Uploading to PyPi...
	twine upload dist/sbapp-*
