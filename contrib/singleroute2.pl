#!/usr/bin/perl -w

# convert traffic files between the MakeRoutes format and the required for PMS

if (@ARGV < 1) {
    printf("singelroute2.pl <topo-file> <mode 0/1> <-r for reverse>  \n");
} else {

    $ROUTE_FILE = "$ARGV[0]-routes-rm$ARGV[1]";
    $PMS_FILE = "$ARGV[0]-routes-pms$ARGV[1]";
    $REVERSE = $ARGV[2];
    if (!defined($REVERSE)) {
	$REVERSE = "-1";
    }

    $flow_count = 0;
    open(INF,"$ROUTE_FILE") or die "Fehler beim öffnen von $ROUTE_FILE: $!\n";
    while(defined($zeile = <INF>)) {
        if ($zeile=~/flow /) {
            $flow_count = $flow_count + 1;
        }
    }
    printf("Flows: $flow_count \n");
    close(INF) or die "Fehler beim Schliessen von $ARGV[0]: $! \n";

    open(INF,"$ROUTE_FILE") or die "Fehler beim öffnen von $ROUTE_FILE: $!\n"; 
    open(OUT,"> $PMS_FILE") or die "Fehler beim öffnen von $PMS_FILE: $!\n";

    if ($REVERSE eq "-r") {
	
	while (<INF>) {
	    chomp $_;
	    my @a = split(/ +/, $_);
	    printf(OUT "flow $a[0] $a[$#a] \n");
	    my $i;
	    for ($i = 0; $i < $#a; $i++) {
		printf(OUT "link $a[$i] $a[$i+1] \n");
	    }
	}

    } else {

	printf(OUT "$flow_count 0 \n");

	my %tmp;
	my $stop = 1;
	while (<INF>) {
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
		if (defined($start)&&($start == 1)) {
		    printf(OUT "$src");
		    undef $start;
		    undef $stop;
		}
		my $i = shift(@a);
		@{$tmp{$i}} = @a;
		while (defined $tmp{$src}) {
		    @a = @{$tmp{$src}};
		    my $j = $a[int(($#a + 1) * rand)];
		    printf(OUT " $j");
		    $src = $j;
		    if ($j eq $dst) {
			printf(OUT "\n");
			$stop = 1;
		    }
		}
	    }
	}
    }
}
