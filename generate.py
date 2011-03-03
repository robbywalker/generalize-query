#!/usr/bin/env python

"""Quick python script to generate test data."""

import random

PATTERNS = [
  ('%(first_name)s', 10),
  ('%(word)s', 10),
  ('%(place)s', 5),
  ('%(first_name)s %(last_name)s', 3),
  ('%(place)s %(word)s', 2),
  ('%(word)s %(place)s', 2),
  ('%(word)s %(place)s %(word)s', 1),
  ('%(first_name)s %(word)s', 1),
]

def file_list(filename):
	with open(filename) as f:
		return [x.strip() for x in f if x.strip()]
		
		
def choice_prefer_front(arr):
	index = random.randrange(len(arr))
	index = index and random.randrange(index)
	return arr[index]


def weighted_choice(arr):
	total = sum((x[1] for x in arr))
	value = random.randrange(total)
	for elem in arr:
		if value < elem[1]:
			return elem[0]
		value -= elem[1]
	raise Exception('Should never get here')


def main():
	firstnames = file_list('data/firstnames.csv')[0].split()
	lastnames = file_list('data/lastnames.csv')[0].split()
	places = file_list('data/places.txt')
	words = [line.split(',')[0] for line in file_list('data/words.csv')]
	
	for i in xrange(50000):
		firstname = random.choice(firstnames)
		lastname = random.choice(lastnames)
		place = random.choice(places)
		word = choice_prefer_front(words)
		pattern = weighted_choice(PATTERNS)
		print pattern % {'first_name': firstname, 'last_name': lastname, 'word': word, 'place': place}


if __name__ == '__main__':
	main()
