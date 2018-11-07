#!/usr/bin/perl

# convert traffic files between the MakeRoutes format and the required for PMS

if ($ARGV[0] eq "-r") {

	while (<STDIN>) {
		chomp $_;
		my @a = split(/ +/, $_);
		print "flow ".$a[0]." ".$a[$#a]."\n";
		my $i;
		for ($i = 0; $i < $#a; $i++) {
			print "link ".$a[$i]." ".$a[$i+1]."\n";
		}
	}

} else {

	my %tmp;
	my $stop = 1;
	while (<STDIN>) {
		chomp $_;
		my @a = split(/ +/, $_);
		my $key = shift(@a);
		if ($key eq "flow") {
			if (! defined $stop) {
				die "hurga!\n";
			}
			$src = $a[0];
			$dst = $a[1];
			$start = 1;
			%tmp = ();
		} elsif ($key eq "link") {
			if ($start == 1) {
				print $src;
				undef $start;
				undef $stop;
			}
			my $i = shift(@a);
			@{$tmp{$i}} = @a;
			while (defined $tmp{$src}) {
				@a = @{$tmp{$src}};
				my $j = $a[int(($#a + 1) * rand)];
				print " $j";
				$src = $j;
				if ($j eq $dst) {
					print "\n";
					$stop = 1;
				}
			}
		}
	}

}
