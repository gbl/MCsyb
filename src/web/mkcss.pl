#!/usr/bin/perl

$cellsize=$ARGV[0] || 16;
$cellsize++;

for ($i=0; $i<256; $i++) {
	for ($j=0; $j<16; $j++) {
		printf "td.c%02x%x { background-position: -%dpx -%dpx; }\n",
			$i, $j, $j*$cellsize, $i*$cellsize;
	}
}
