import getopt
import sys

#Simple stub script used to test whether ProcessControl will run/read-from a python script
def main(): 
	_, args = getopt.getopt(sys.argv[1:], "", [])
	if args[0]: print (args[0].upper());

if __name__ == "__main__":
	main()