//go:build android

package libcore

import (
	"fmt"
	"io"
	"log"
	"os"
	"strconv"

	"golang.org/x/mobile/asset"
)

func extractAssets() {
	useOfficialAssets := intfNB4A.UseOfficialAssets()

	extract := func(name string) {
		err := extractAssetName(name, useOfficialAssets)
		if err != nil {
			log.Println("Extract", name, "failed:", err)
		}
	}

	extract(geoipDat)
	extract(geositeDat)
	extract(metacubexdDstFolder)
}

func extractAssetName(name string, useOfficialAssets bool) error {
	replaceable := true

	var version string
	var apkPrefix string
	switch name {
	case geoipDat:
		version = geoipVersion
		apkPrefix = apkAssetPrefixSingBox
	case geositeDat:
		version = geositeVersion
		apkPrefix = apkAssetPrefixSingBox
	case metacubexdDstFolder:
		version = metacubexdVersion
		replaceable = false
	}

	var dir string
	if !replaceable {
		dir = internalAssetsPath
	} else {
		dir = externalAssetsPath
	}
	dstName := dir + name
	tmpDstName := dstName + ".tmp"

	var localVersion string
	var assetVersion string

	loadAssetVersion := func() error {
		av, err := asset.Open(apkPrefix + version)
		if err != nil {
			return fmt.Errorf("open version in assets: %v", err)
		}
		b, err := io.ReadAll(av)
		av.Close()
		if err != nil {
			return fmt.Errorf("read internal version: %v", err)
		}
		assetVersion = string(b)
		return nil
	}
	if err := loadAssetVersion(); err != nil {
		return err
	}

	var doExtract bool

	if _, err := os.Stat(dstName); err != nil {
		doExtract = true
	} else if useOfficialAssets || !replaceable {
		b, err := os.ReadFile(dir + version)
		if err != nil {
			doExtract = true
			_ = os.RemoveAll(dir + version)
		} else {
			localVersion = string(b)
			av, err := strconv.ParseUint(assetVersion, 10, 64)
			if err != nil {
				doExtract = assetVersion != localVersion
			} else {
				lv, err := strconv.ParseUint(localVersion, 10, 64)
				doExtract = err != nil || av > lv
			}
		}
	}

	if !doExtract {
		return nil
	}

	extractXz := func(f asset.File) error {
		tmpXzName := dstName + ".xz"
		err := extractAsset(f, tmpXzName)
		if err == nil {
			err = Unxz(tmpXzName, dstName)
			os.Remove(tmpXzName)
		}
		if err != nil {
			return fmt.Errorf("extract xz: %v", err)
		}
		return nil
	}

	extractTarGz := func(f asset.File, outDir string) error {
		tmpTarGzName := tmpDstName + ".tgz"
		err := extractAsset(f, tmpTarGzName)
		if err == nil {
			err = UntarGz(tmpTarGzName, outDir)
			os.Remove(tmpTarGzName)
		}
		if err != nil {
			return fmt.Errorf("extract tgz: %v", err)
		}
		return nil
	}

	if f, err := asset.Open(apkPrefix + name + ".xz"); err == nil {
		if err := extractXz(f); err != nil {
			return err
		}
	} else if f, err := asset.Open("metacubexd.tgz"); err == nil {
		_ = os.RemoveAll(tmpDstName)
		if err := os.MkdirAll(tmpDstName, 0o755); err != nil {
			return fmt.Errorf("mkdir metacubexd temp dir: %v", err)
		}
		if err := extractTarGz(f, tmpDstName); err != nil {
			_ = os.RemoveAll(tmpDstName)
			return err
		}
		_ = os.RemoveAll(dstName)
		if err := os.Rename(tmpDstName, dstName); err != nil {
			_ = os.RemoveAll(tmpDstName)
			return fmt.Errorf("commit metacubexd: %v", err)
		}
	}

	o, err := os.Create(dir + version)
	if err != nil {
		return fmt.Errorf("create version: %v", err)
	}
	_, err = io.WriteString(o, assetVersion)
	o.Close()
	return err
}

func extractAsset(i asset.File, path string) error {
	defer i.Close()
	o, err := os.Create(path)
	if err != nil {
		return err
	}
	defer o.Close()
	_, err = io.Copy(o, i)
	if err == nil {
		log.Println("Extract >>", path)
	}
	return err
}
