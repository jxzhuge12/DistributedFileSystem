import operator

f = open("result")

data = {}
for line in f:
    line = line.split()
    data[line[0] + " " + line[1]] = int(line[2])

sorted_data = list(reversed(sorted(data.items(), key=operator.itemgetter(1))))

num = 0
for i in range(len(sorted_data)):
    num += sorted_data[i][1]

print "the total number of bigrams: ", num
print "the most common bigram: "
max_num = sorted_data[0][1]
for i in range(len(sorted_data)):
    if sorted_data[i][1] == max_num:
        print sorted_data[i][0]
print "the number of bigrams required to add up to 10% of all bigrams"
total = num * 0.1
for i in range(len(sorted_data)):
    print sorted_data[i][0]
    total -= sorted_data[i][1]
    if total < 0: break
        