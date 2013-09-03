from PIL import Image

import sys
import getopt

class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

def resizeImageMaxSide(inFile, outFile, maxSide, orientation):
	im = Image.open(inFile)
	#first resize
	width, height = im.size
	ratio = width / float(height)
	if ratio > 1:
		im = im.resize((maxSide, maxSide / ratio), Image.ANTIALIAS)
	else:
		im = im.resize((maxSide * ratio, maxSide), Image.ANTIALIAS)
	#then rotate if need be
	if orientation == 6:
		im = im.transpose(Image.ROTATE_270)
	elif orientation == 8:
		im = im.transpose(Image.ROTATE_90)
	im.save(outFile)


def resizeImageConstrainedSize(inFile, outFile, constraintSize, orientation, quality = 100):
	im = Image.open(inFile)
	#first resize, to fit the constraints
	if orientation == 1 or orientation == 0:
		width, height = im.size
	elif orientation == 6 or orientation == 8:
		height, width = im.size
	widthRatio = constraintSize[0]/float(width)
	heightRatio = constraintSize[1]/float(height)
	if widthRatio < heightRatio:
		newSize = (constraintSize[0], height * widthRatio)
	else:
		newSize = (width * heightRatio, constraintSize[1])
	if orientation == 6 or orientation == 8:
		im = im.resize((newSize[1],newSize[0]), Image.ANTIALIAS)	
	else:
		im = im.resize(newSize, Image.ANTIALIAS)
	#now, rotate if need be
	if orientation == 6:
		im = im.transpose(Image.ROTATE_270)
	elif orientation == 8:
		im = im.transpose(Image.ROTATE_90)
	im.save(outFile, quality = quality)


def main(argv=None):
    if argv is None:
        argv = sys.argv
    try:
        try:
            opts, args = getopt.getopt(argv[1:], "h", ["help"])
        except getopt.error, msg:
             raise Usage(msg)
        if args[0] == "resizeImageMaxSide" and len(args) == 5:
        	resizeImageMaxSide(args[1],args[2],float(args[3]),int(args[4]))
        elif args[0] == "resizeImageConstrainedSize" and len(args) == 6:
        	resizeImageConstrainedSize(args[1],args[2], (float(args[3]), float(args[4])), int(args[5]))
    except Usage, err:
        print >>sys.stderr, err.msg
        print >>sys.stderr, "for help use --help"
        return 2

if __name__ == "__main__":
    sys.exit(main())

