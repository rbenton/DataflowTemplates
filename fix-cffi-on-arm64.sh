# see https://stackoverflow.com/a/78097593

set -e

CFFI_SO=~/.config/gcloud/virtenv/lib/python3.11/site-packages/_cffi_backend.cpython-311-darwin.so

if lipo "$CFFI_SO" -archs | grep -q 'x86_64'; then
  echo "$CFFI_SO already contains an x86_64 binary."
  exit
fi

cp "$CFFI_SO" ~/Downloads/tmp_arm64.so

source ~/.config/gcloud/virtenv/bin/activate
arch -x86_64 pip install cffi --compile --force-reinstall --no-cache-dir

lipo -create -output "$CFFI_SO" ~/Downloads/tmp_arm64.so "$CFFI_SO"
