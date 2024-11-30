import pandas

STRIDE = 100
WINDOW = 200


def μ(x):
    return sum(x) / len(x)


def σ2(x):
    mean = μ(x)
    return sum((xi - mean) ** 2 for xi in x) / len(x)


def tefs(x):
    return sum([xi**2 for xi in x]) / len(x)


def corr(x, y):
    assert len(x) == len(y) and len(x) > 0
    mean_x = μ(x)
    mean_y = μ(y)
    return sum((xi - mean_x) * (yi - mean_y) for xi, yi in zip(x, y)) / len(x)


def read_csv_to_2d_list(file_path):
    df = pandas.read_csv(file_path)
    return df.values.tolist()


if __name__ == "__main__":
    with open("sensor_data", "w") as f:
        values = [[] for _ in range(0, 7)]
        for label in range(0, 7):
            gDat = read_csv_to_2d_list(f"./{label}/gravity.csv")
            lDat = read_csv_to_2d_list(f"./{label}/linear.csv")
            zDat = read_csv_to_2d_list(f"./{label}/gyro.csv")

            print(len(gDat), len(lDat), len(zDat))

            gDat = gDat[0 : min(len(gDat), len(lDat), len(zDat))]
            lDat = lDat[0 : min(len(gDat), len(lDat), len(zDat))]
            zDat = zDat[0 : min(len(gDat), len(lDat), len(zDat))]

            gX = [d[2] for d in gDat]
            gY = [d[3] for d in gDat]
            gZ = [d[4] for d in gDat]

            lX = [d[2] for d in lDat]
            lY = [d[3] for d in lDat]
            lZ = [d[4] for d in lDat]

            zX = [d[2] for d in zDat]
            zY = [d[3] for d in zDat]
            zZ = [d[4] for d in zDat]

            for i in range(0, len(gX) - WINDOW, STRIDE):
                gXWindow = gX[i : i + WINDOW]
                gYWindow = gY[i : i + WINDOW]
                gZWindow = gZ[i : i + WINDOW]

                lXWindow = lX[i : i + WINDOW]
                lYWindow = lY[i : i + WINDOW]
                lZWindow = lZ[i : i + WINDOW]

                zXWindow = zX[i : i + WINDOW]
                zYWindow = zY[i : i + WINDOW]
                zZWindow = zZ[i : i + WINDOW]

                string = f"{label} "
                for idx, val in enumerate(
                    [
                        μ(gXWindow),
                        μ(gYWindow),
                        μ(gZWindow),
                        σ2(gXWindow),
                        σ2(gYWindow),
                        σ2(gZWindow),
                        tefs(gXWindow),
                        tefs(gYWindow),
                        tefs(gZWindow),
                        corr(gXWindow, gYWindow),
                        corr(gXWindow, gZWindow),
                        corr(gYWindow, gZWindow),
                        μ(lXWindow),
                        μ(lYWindow),
                        μ(lZWindow),
                        σ2(lXWindow),
                        σ2(lYWindow),
                        σ2(lZWindow),
                        tefs(lXWindow),
                        tefs(lYWindow),
                        tefs(lZWindow),
                        corr(lXWindow, lYWindow),
                        corr(lXWindow, lZWindow),
                        corr(lYWindow, lZWindow),
                        μ(zXWindow),
                        μ(zYWindow),
                        μ(zZWindow),
                        σ2(zXWindow),
                        σ2(zYWindow),
                        σ2(zZWindow),
                        tefs(zXWindow),
                        tefs(zYWindow),
                        tefs(zZWindow),
                        corr(zXWindow, zYWindow),
                        corr(zXWindow, zZWindow),
                        corr(zYWindow, zZWindow),
                    ]
                ):
                    string += f"{1+idx}:{val} "

                values[label].append(string)

        print([len(x) for x in values])

        for i in range(min([len(x) for x in values])):
            for label in range(0, 7):
                if i < len(values[label]):
                    f.write(values[label][i] + "\n")
