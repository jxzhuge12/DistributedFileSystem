lines = 0
fail_times = 0
with open("output", "r") as f:
    for line in f:
	lines += 1
        if line != "Pong 777\n":
	    fail_times += 1
fail_times += (4 - lines)
if fail_times == 0:
    print "4 Tests Completed, 0 Tests Failed"
else:
    print str(4-fail_times) + " Tests Completed, " + str(fail_times) + " Tests Failed"
